package com.github.lisdocument.msio.config;

import com.github.lisdocument.msio.bean.db.DownloadReword;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 存储下载记录的，仅有被传入dataSource的时候才会开启功能
 * @author Libin
 * @version 1.0.1
 */
@Configuration
@ConditionalOnMissingBean(AbstractStoreRecordConfigure.class)
public class StoreRecordConfiguration extends AbstractStoreRecordConfigure<DataSource> {

    @Value("${spring.msIo.tableName:}")
    private String tableName;

    private String insertSql;

    @Autowired
    StoreRecordConfiguration(AbstractMsConfigure abstractMsConfigure){
        dataSource = abstractMsConfigure.configDataSource();
        if(null == dataSource){
            log.info("no datasource autowired，autoStore stop");
        }else{
            log.info("datasource autowired，autoStore start");
            init();
        }
    }

    @Override
    protected void save(DownloadReword downloadReword) {
        //todo
        try (Connection c = dataSource.getConnection();
             PreparedStatement preparedStatement = c.prepareStatement(insertSql)){
            preparedStatement.setObject(1,downloadReword.getUsername());
            preparedStatement.setObject(2,downloadReword.getId());
            preparedStatement.setObject(3,downloadReword.getIp());
            preparedStatement.setObject(4,downloadReword.getTime());
            preparedStatement.setObject(5,downloadReword.getUrl());
            preparedStatement.setObject(6,downloadReword.getParams());
            preparedStatement.setObject(7,downloadReword.getCostTime());
            preparedStatement.setObject(8,downloadReword.getMethod());
            boolean execute = preparedStatement.execute();
            if(!execute){
                log.error("当前一条记录插入失败：插入对象->" + downloadReword + " 插入语句范例->" + insertSql);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void init() {
        log.info("请确认数据源中是否存在记录表结构，不存在将会在导出操作时报错");
        log.info("plz check your database,if there are not a table with correct column, it will be throw a Exception where you download");
        //构建初始化方法
        insertSql = "insert into "+ tableName +"('username','id','ip','time','url','params','costTime','method') values(?,?,?,?,?,?,?,?)";
    }
}
