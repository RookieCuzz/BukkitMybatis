package com.cuzz.bukkitmybatis.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.datasource.pooled.PooledDataSourceFactory;

public class HikariDataSourceFactory extends PooledDataSourceFactory {

    public HikariDataSourceFactory(){
        this.dataSource= new HikariDataSource();
    }
}
