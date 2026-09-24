package com.yato.urlShortenerb.repo;

import com.yato.urlShortenerb.entity.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UrlRepo extends JpaRepository<Url,Long> {
    Optional<Url> findByShortCode(String shortCode);
    List<Url> findByUserId(Long userId);
    Page<Url> findByUserId(Long userId, Pageable pageable);

    // Single UPDATE so concurrent clicks are never lost (read-modify-write was racy)
    @Modifying
    @Query("UPDATE Url u SET u.clickCount = COALESCE(u.clickCount, 0) + 1 WHERE u.id = :id")
    int incrementClickCount(Long id);

}
