package com.jagrosh.jmusicbot.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.queue.FairQueue;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;



public class SaveQueueCmd extends MusicCommand {

    public SaveQueueCmd(Bot bot){
        super(bot);
        this.name = "savequeue";
        this.help = "Saves current queue. Please only use ASCII characters for `name`.";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.arguments = "<name>";
        this.beListening = true;
        this.bePlaying = false;
    }

    @Override
    public void doCommand(CommandEvent event) {
        AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
        String plname = event.getArgs();
        assert handler != null;
        int saved = save(handler.getQueue(), plname);
        switch (saved){
            case 0:
            event.reply("Queue empty, couldn't save. Use `" + bot.getConfig().getPrefix() + "play` to add songs.");
            return;

            case -1:
            event.reply("Name not entered `" + bot.getConfig().getPrefix() + this.name +" <name>`");
            return;

            default:
            event.reply("Saved queue with `" + saved + "` entries");
        }
    }

    private int save(FairQueue playlist, String name)  {
        if (!name.isEmpty()) {
            try {
                for (int i=0; i<playlist.size(); i++){
                    String pl = String.valueOf(playlist.get(i));
                    pl = pl.substring(pl.indexOf("https:", 0), pl.indexOf("@", 0));
                    pl = pl.substring(0, 43) + "\n";
                    FileUtils.writeStringToFile(new File("Playlists/"+ name + ".txt"), pl, true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return playlist.size();
        }
        else return -1;
    }
}
