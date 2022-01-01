package com.jagrosh.jmusicbot.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;


public class UnskipCmd extends MusicCommand {

    public UnskipCmd(Bot bot) {
        super(bot);
        this.name = "unskip";
        this.beListening = true;
        this.bePlaying = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        int e = handler.unskip();
        if (e==1){
            event.replySuccess("Playing previous song");
        }
        else {event.replyError("Nothing previously played song found");}




    }
}
