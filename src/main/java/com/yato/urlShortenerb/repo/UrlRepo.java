package com.yato.urlShortenerb.repo;

import com.yato.urlShortenerb.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UrlRepo extends JpaRepository<Url,Long> {
    Optional<Url> findByShortCode(String shortCode);
    List<Url> findByUserId(Long userId);

}
