package com.jagrosh.jmusicbot.commands.owner;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.commands.MusicCommand;

public class ListGuilds extends MusicCommand {

    private final String[] a = {"lg"};
    private boolean alone = false;

    public ListGuilds(Bot bot) {
        super(bot);
        this.hidden = true;
        this.name = "listguidls";
        this.aliases = a;
    }

    @Override
    public void doCommand(CommandEvent event) {
        if (event.getAuthor().equals(bot.getJDA().getUserById(bot.getConfig().getOwnerId()))) // lidl owner command
        alone = false;
        {
            bot.getJDA().getGuilds().forEach(g -> {
                if (g.getAudioManager().isConnected()){
                    alone = true;
                    event.reply("Guild: `" + g.getName() +"`\nOwner: `" + g.getOwner().getUser().getName() + "`\n---------------------------");
                }
            });
            if (!alone) {
                event.replyError("Not playing anywhere");
            }
        }
    }
}
