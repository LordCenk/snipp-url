package com.yato.urlShortenerb.repo;

import com.yato.urlShortenerb.entity.AnalyticsEvent;
import com.yato.urlShortenerb.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnalyticsEventRepo extends JpaRepository<AnalyticsEvent, Long> {

    @Query("""
        SELECT COUNT(a)
        FROM AnalyticsEvent a
        WHERE a.url.user.id = :userId
        """)
    long countByUserId(Long userId);

    @Query("""
        SELECT FUNCTION('DATE', a.timestamp) AS day, COUNT(a)
        FROM AnalyticsEvent a
        WHERE a.url.user.id = :userId
        GROUP BY FUNCTION('DATE', a.timestamp)
        ORDER BY day
        """)
    List<Object[]> countClicksPerDay(Long userId);

    @Query("""
        SELECT a.device, COUNT(a)
        FROM AnalyticsEvent a
        WHERE a.url.user.id = :userId
        GROUP BY a.device
        """)
    List<Object[]> countDevices(Long userId);

    @Query("""
        SELECT a.referrer, COUNT(a)
        FROM AnalyticsEvent a
        WHERE a.url.user.id = :userId
        GROUP BY a.referrer
        """)
    List<Object[]> countReferrers(Long userId);

    @Modifying
    @Query("DELETE FROM AnalyticsEvent a WHERE a.url = :url")
    void deleteByUrl(Url url);
}
