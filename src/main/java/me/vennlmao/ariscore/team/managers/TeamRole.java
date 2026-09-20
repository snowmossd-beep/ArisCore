package me.vennlmao.ariscore.team.managers;

import me.vennlmao.ariscore.team.TeamModule;

public enum TeamRole {

    LEADER,
    ADMIN,
    MEMBER;

    public boolean has(TeamModule module, String permission) {
        if (this == LEADER) return true;
        return module.getConfig().getBoolean("roles." + name().toLowerCase() + "." + permission, false);
    }

    public String display(TeamModule module) {
        return module.getConfig().getString("roles." + name().toLowerCase() + ".display-name", name());
    }

    public String icon(TeamModule module) {
        return module.getConfig().getString("roles." + name().toLowerCase() + ".icon", "");
    }
                                            }
