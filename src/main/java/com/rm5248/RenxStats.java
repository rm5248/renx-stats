package com.rm5248;


import com.owlike.genson.GenericType;
import com.owlike.genson.Genson;
import com.rm5248.json.ServerInformation;
import com.rm5248.renxstats.generated.Tables;
import com.rm5248.renxstats.generated.tables.records.RenxstatsRecord;
import com.rm5248.renxstats.generated.tables.records.ServersRecord;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.sqlite.SQLiteConfig;

public class RenxStats {
    
    private static final Logger logger = LogManager.getLogger();
    
    private DSLContext m_context;
    private Connection m_sqlConnection;
    
    private RenxStats( Properties props ) throws SQLException, KeyStoreException {
        SQLiteConfig config = new SQLiteConfig();
        config.setPragma(SQLiteConfig.Pragma.FOREIGN_KEYS, "on" );
        m_sqlConnection = DriverManager.getConnection( props.getProperty( "db.url" ), config.toProperties() );
        Flyway flyway = Flyway.configure()
                .dataSource(props.getProperty( "db.url" ), null, null)
                .mixed(true)
                .load();
        flyway.migrate();
        
        m_context = DSL.using( m_sqlConnection );
        
        logger.info( "Starting up RenxStats" );
    }
    
    void run() throws IOException, InterruptedException {
        OkHttpClient client = new OkHttpClient();
        Genson genson = new Genson();

        Request request = new Request.Builder()
            .url("https://serverlist.renegade-x.com/servers.jsp?id=launcher")
            .addHeader( "User-Agent:", "RenX-Launcher (0.84)" )
            .build();

        while( true ){
            LocalDateTime now = LocalDateTime.now();
            Response response = client.newCall(request).execute();
            String strResponse = response.body().string();
            logger.debug( "got {}", strResponse );

            List<ServerInformation> servers = 
                    genson.deserialize( strResponse, new GenericType<List<ServerInformation>>(){});
            m_context.transaction( configuration -> {
                for( ServerInformation i : servers ){
                    logger.debug( "Server name: {} Players: {}", i.Name, i.Players);
                    
                    ServersRecord serversRec = DSL.using( configuration )
                            .selectFrom( Tables.SERVERS )
                            .where( Tables.SERVERS.SERVERNAME.eq( i.Name ) )
                            .fetchAny();
                    
                    if( serversRec == null ){
                        serversRec = DSL.using( configuration )
                                .newRecord( Tables.SERVERS );
                        serversRec.setServername( i.Name );
                        serversRec.store();
                    }

                    // Insert a value into our reading table so we know how many people are in the server
                    RenxstatsRecord newrec = DSL.using( configuration )
                            .newRecord( Tables.RENXSTATS );
                    newrec.setPlayersinserver( i.Players );
                    newrec.setRecordingtime( now );
                    newrec.setServersServerid( serversRec.getServerid() );
                    newrec.store();
                }
            });
        
            Thread.sleep( 1000 * 60 * 5 /* five minutes */ );
        }

    }
    
    public static void main(String[] args){
        Properties props;

        props = new Properties();
        props.setProperty( "db.url", "jdbc:sqlite:file:./renxstats.db" );

        try {
            RenxStats ix = new RenxStats( props );
            ix.run();
        } catch (IOException ex) {
            logger.fatal("Exiting due to IO exception: ", ex);
        } catch (SQLException ex) {
            logger.fatal("SQL Exception: ", ex );
        } catch( Exception ex ){
            logger.fatal( "other exception ", ex );
        }
        
        System.exit( 1 );
    }
}
