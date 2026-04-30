package com.stelian.modpack_service.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "modpack_owners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModpackOwner {
    @Id
    private String ownerToken; // Token-ul generat (ex: usr_abcd123)

    private String nickname;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    @JsonBackReference
    private List<Modpack> modpacks = new ArrayList<>();
}