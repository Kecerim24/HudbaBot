package com.jagrosh.jmusicbot.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.entities.Pair;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.jagrosh.jmusicbot.utils.SpotifyUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class SpotifyCmd extends MusicCommand {

    private final String loadingEmoji;
    SpotifyUtil su = new SpotifyUtil();
    private final static String LOAD = "\uD83D\uDCE5"; // 📥
    private final static String CANCEL = "\uD83D\uDEAB"; // 🚫


    public SpotifyCmd(Bot bot){
        super(bot);
        this.loadingEmoji = bot.getConfig().getLoading();
        this.name = "spotify";
        this.help = "Plays song or playlist from spotify";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.arguments = "<Spotify URL>";
        this.beListening = true;
        this.bePlaying = false;
    }

    @Override
    public void doCommand(CommandEvent event) {

        String args = event.getArgs();


        if (args.contains("track")){
            args = args.replace("https://open.spotify.com/track/", "").replaceAll("\\?(.*)", "");
            String song = su.getTrack(args).getName();
            CommandEvent finalEvent = new CommandEvent(event.getEvent(), song, event.getClient());
            event.reply(loadingEmoji + " Loading... `["+song+"]`", m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), song, new ResultHandler(m, finalEvent,false)));
        }
        else if (args.contains("playlist")){
            args = args.replace("https://open.spotify.com/playlist/", "").replaceAll("\\?(.*)", "");
            ArrayList<Pair<String, String>> playlist = su.getPlaylist(args);
            int playlistSize = playlist.size();
            try {
                playlist.forEach((pair) -> {
                    try {
                        String song = pair.getKey() + " " + pair.getValue();
                        CommandEvent finalEvent = new CommandEvent(event.getEvent(), song, event.getClient());
                        bot.getPlayerManager().loadItemOrdered(event.getGuild(), song, new ResultHandler(null, finalEvent, false));

                    } catch (Exception ex) {
                        System.out.println("Error loading: " + ex.getCause());
                    }
                });
            }catch (Exception e){ System.out.println(e.getMessage());}


            event.replySuccess("Spotify playlist with `" + playlistSize + "` songs loaded.");


        }
        else event.replyError("Invalid link");


    }

    private class ResultHandler implements AudioLoadResultHandler
    {
        private final Message m;
        private final CommandEvent event;
        private final boolean ytsearch;


        private ResultHandler(Message m, CommandEvent event, boolean ytsearch)
        {
            this.m = m;
            this.event = event;
            this.ytsearch = ytsearch;

        }

        private void loadSingle(AudioTrack track, AudioPlaylist playlist)
        {
            if(bot.getConfig().isTooLong(track))
            {
                m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" This track (**"+track.getInfo().title+"**) is longer than the allowed maximum: `"
                        +FormatUtil.formatTime(track.getDuration())+"` > `"+FormatUtil.formatTime(bot.getConfig().getMaxSeconds()*1000)+"`")).queue();
                return;
            }
            AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
            int pos = handler.addTrack(new QueuedTrack(track, event.getAuthor()))+1;

                String addMsg = FormatUtil.filter(event.getClient().getSuccess()+" Added **"+track.getInfo().title
                        +"** (`"+FormatUtil.formatTime(track.getDuration())+"`) "+(pos==0?"to begin playing":" to the queue at position "+pos));
                if(playlist==null || !event.getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_ADD_REACTION))
                    m.editMessage(addMsg).queue();

                else
                {
                    new ButtonMenu.Builder()
                            .setText(addMsg+"\n"+event.getClient().getWarning()+" This track has a playlist of **"+playlist.getTracks().size()+"** tracks attached. Select "+LOAD+" to load playlist.")
                            .setChoices(LOAD, CANCEL)
                            .setEventWaiter(bot.getWaiter())
                            .setTimeout(30, TimeUnit.SECONDS)
                            .setAction(re ->
                            {
                                if(re.getName().equals(LOAD))
                                    m.editMessage(addMsg+"\n"+event.getClient().getSuccess()+" Loaded **"+loadPlaylist(playlist, track)+"** additional tracks!").queue();
                                else
                                    m.editMessage(addMsg).queue();
                            }).setFinalAction(m ->
                            {
                                try{ m.clearReactions().queue(); }catch(PermissionException ignore) {}
                            }).build().display(m);
                }
            }



        private int loadPlaylist(AudioPlaylist playlist, AudioTrack exclude)
        {
            int[] count = {0};
            playlist.getTracks().stream().forEach((track) -> {
                if(!bot.getConfig().isTooLong(track) && !track.equals(exclude))
                {
                    AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
                    handler.addTrack(new QueuedTrack(track, event.getAuthor()));
                    count[0]++;
                }
            });
            return count[0];
        }

        @Override
        public void trackLoaded(AudioTrack track)
        {
            loadSingle(track, null);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist)
        {
            if(playlist.getTracks().size()==1 || playlist.isSearchResult())
            {
                AudioTrack single = playlist.getSelectedTrack()==null ? playlist.getTracks().get(0) : playlist.getSelectedTrack();
                loadSingle(single, null);
            }
            else if (playlist.getSelectedTrack()!=null)
            {
                AudioTrack single = playlist.getSelectedTrack();
                loadSingle(single, playlist);
            }
            else
            {
                int count = loadPlaylist(playlist, null);
                if(count==0)
                {
                    m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" All entries in this playlist "+(playlist.getName()==null ? "" : "(**"+playlist.getName()
                            +"**) ")+"were longer than the allowed maximum (`"+bot.getConfig().getMaxTime()+"`)")).queue();
                }
                else
                {
                    m.editMessage(FormatUtil.filter(event.getClient().getSuccess()+" Found "
                            +(playlist.getName()==null?"a playlist":"playlist **"+playlist.getName()+"**")+" with `"
                            + playlist.getTracks().size()+"` entries; added to the queue!"
                            + (count<playlist.getTracks().size() ? "\n"+event.getClient().getWarning()+" Tracks longer than the allowed maximum (`"
                            + bot.getConfig().getMaxTime()+"`) have been omitted." : ""))).queue();
                }
            }
        }

        @Override
        public void noMatches()
        {
            if(ytsearch)
                m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" No results found for `"+event.getArgs()+"`.")).queue();
            else
                bot.getPlayerManager().loadItemOrdered(event.getGuild(), "ytsearch:"+event.getArgs(), new ResultHandler(m,event,true));
        }

        @Override
        public void loadFailed(FriendlyException throwable)
        {
            if(throwable.severity== FriendlyException.Severity.COMMON)
                m.editMessage(event.getClient().getError()+" Error loading: "+throwable.getMessage()).queue();
            else
                m.editMessage(event.getClient().getError()+" Error loading track.").queue();
        }
    }
}