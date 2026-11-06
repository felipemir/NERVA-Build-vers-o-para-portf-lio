package com.nervatech.nerva_build.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data // Gera Getters, Setters e toString automaticamente
@Entity // Diz pro Spring: "Isso é uma tabela no banco"
@Table(name = "store")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String subdomain; // ex: "obraja"

    private Boolean active = true;

    @Column(name = "marketplace_visible", nullable = false)
    private Boolean marketplaceVisible = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
