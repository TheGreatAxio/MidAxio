package com.axios.midaxio.entity;

import com.axios.midaxio.model.LeagueRegion;
import com.axios.midaxio.model.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@NullMarked
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String email;

    @Column(unique = true, nullable = true)
    private String puuid;

    @Column(nullable = true)
    private String gameName;

    @Column(nullable = true)
    private String tagLine;

    @Column(nullable = true)
    private String resetToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private LeagueRegion leagueRegion;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean isEmailVerified = false;
    private boolean isIgnVerified = false;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @NotNull
    @Override
    public String getUsername() { return email; }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return isEmailVerified; }

}