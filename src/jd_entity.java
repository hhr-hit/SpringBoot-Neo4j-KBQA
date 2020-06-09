package com.hhr.node;

import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class jd_entity extends BaseEntity {

    private String name;

    public jd_entity(){

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
