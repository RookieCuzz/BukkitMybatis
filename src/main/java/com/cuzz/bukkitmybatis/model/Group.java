package com.cuzz.bukkitmybatis.model;

public class Group {
    private Integer id;
    private String name;
    private String leader;

    @Override
    public String toString() {
        return "Group{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", leader='" + leader + '\'' +
                ", leaderId=" + leaderId +
                ", sort=" + sort +
                '}';
    }

    private Integer leaderId;

    private Integer sort;


    public String getName(){
        return this.name;
    }
}
