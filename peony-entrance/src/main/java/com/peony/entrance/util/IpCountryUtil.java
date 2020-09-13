package com.peony.entrance.util;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.Country;
import com.peony.common.tool.util.ClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;

/**
 * @Author: zhengyuzhen
 * @Date: 2020-09-13 09:25
 */
public class IpCountryUtil {
    private static final Logger log = LoggerFactory.getLogger(IpCountryUtil.class);

    private static FileInputStream database = null;
    private static DatabaseReader reader = null;

    static {
        try {
//            database = new FileInputStream("config/GeoIP2-Country.mmdb");
            database = new FileInputStream(ClassUtil.getClassLoader().getResource("GeoIP2-Country.mmdb").getPath());
            reader = new DatabaseReader.Builder(database).build();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (database != null) {
                try {
                    database.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getCountryCode(String ip) {
        String countryCode = null;
        if (reader != null) {
            try {
                InetAddress ipAddress = InetAddress.getByName(ip);
                CountryResponse response = reader.country(ipAddress);

                Country country = response.getCountry();
                countryCode = country.getIsoCode();
            } catch (Exception e) {
//                e.printStackTrace();
                if(e instanceof AddressNotFoundException){
                    // The address 10.1.38.89 is not in the database
                    log.warn(e.getMessage());
                }else{
                    e.printStackTrace();
                }
            }
        } else {
            log.info("GeoIP DatabaseReader is null");
        }
        return countryCode;
    }
}
