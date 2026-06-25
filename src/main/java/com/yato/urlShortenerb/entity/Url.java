package com.yato.urlShortenerb.entity;

import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="urls")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(unique = true,nullable = false)
    private String shortCode;

    @Column(nullable = false,columnDefinition = "text")
    private String longUrl;

    private Long clickCount=0L;

    private LocalDateTime crtAt= LocalDateTime.now();

    private LocalDateTime expiry;
}
