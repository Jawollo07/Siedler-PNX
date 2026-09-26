package de.jawollo07.siedler.tournament;

import de.jawollo07.siedler.SiedlerPlugin;
import java.util.*;

public final class TournamentArenaManager {
    public record Arena(String name, String world, double ax, double ay, double az,
                        double bx, double by, double bz, double sx, double sy, double sz) {}
    private final SiedlerPlugin plugin;
    public TournamentArenaManager(SiedlerPlugin plugin){this.plugin=plugin;}
    public List<String> names(){
        Object value=plugin.getConfig().get("tournament.arenas");
        if(!(value instanceof Map<?,?> map)) return List.of();
        List<String> out=new ArrayList<>();
        for(Object k:map.keySet()) if(k!=null) out.add(String.valueOf(k));
        out.sort(String.CASE_INSENSITIVE_ORDER); return out;
    }
    public Arena get(String name){
        String b="tournament.arenas."+name;
        String world=plugin.getConfig().getString(b+".world","");
        if(world.isBlank()) return null;
        return new Arena(name,world,
            plugin.getConfig().getDouble(b+".spawn-a.x"),plugin.getConfig().getDouble(b+".spawn-a.y"),plugin.getConfig().getDouble(b+".spawn-a.z"),
            plugin.getConfig().getDouble(b+".spawn-b.x"),plugin.getConfig().getDouble(b+".spawn-b.y"),plugin.getConfig().getDouble(b+".spawn-b.z"),
            plugin.getConfig().getDouble(b+".spectator.x"),plugin.getConfig().getDouble(b+".spectator.y"),plugin.getConfig().getDouble(b+".spectator.z"));
    }
    public boolean create(String name){
        if(!name.matches("[A-Za-z0-9_-]{1,32}") || get(name)!=null) return false;
        String b="tournament.arenas."+name;
        set(b+".world","overworld"); set(b+".spawn-a.x",0);set(b+".spawn-a.y",100);set(b+".spawn-a.z",0);
        set(b+".spawn-b.x",10);set(b+".spawn-b.y",100);set(b+".spawn-b.z",0);
        set(b+".spectator.x",5);set(b+".spectator.y",110);set(b+".spectator.z",5);
        return save();
    }
    public boolean delete(String name){
        Object root=plugin.getConfig().get("tournament.arenas");
        if(!(root instanceof Map<?,?> map) || !map.containsKey(name)) return false;
        map.remove(name); return save();
    }
    public boolean set(String path,Object value){
        try { plugin.getConfig().set(path,value); return true; } catch(Exception e){ return false; }
    }
    public boolean save(){try{plugin.getConfig().save();return true;}catch(Exception e){return false;}}
}
