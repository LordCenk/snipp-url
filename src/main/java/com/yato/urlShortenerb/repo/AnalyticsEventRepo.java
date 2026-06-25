package com.yato.urlShortenerb.repo;

import com.yato.urlShortenerb.entity.AnalyticsEvent;
import com.yato.urlShortenerb.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnalyticsEventRepo extends JpaRepository<AnalyticsEvent, Long> {

    @Query("""
        SELECT COUNT(a)
        FROM AnalyticsEvent a
        WHERE a.url IN :urls
        """)
    long countByUrl(List<Url> urls);

    @Query("""
        SELECT FUNCTION('DATE', a.timestamp) AS day, COUNT(a)
        FROM AnalyticsEvent a
        WHERE a.url IN :urls
        GROUP BY FUNCTION('DATE', a.timestamp)
        ORDER BY day
        """)
    List<Object[]> countClicksPerDay(List<Url> urls);

    @Query("""
        SELECT a.device, COUNT(a)
        FROM AnalyticsEvent a
        WHERE a.url IN :urls
        GROUP BY a.device
        """)
    List<Object[]> countDevices(List<Url> urls);

    @Query("""
        SELECT a.referrer, COUNT(a)
        FROM AnalyticsEvent a
        WHERE a.url IN :urls
        GROUP BY a.referrer
        """)
    List<Object[]> countReferrers(List<Url> urls);
}
