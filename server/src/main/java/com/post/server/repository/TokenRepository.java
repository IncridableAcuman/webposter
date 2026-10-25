package com.post.server.repository;

import com.post.server.entity.TokenEntity;
import com.post.server.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface TokenRepository extends JpaRepository<TokenEntity,Long> {
    Optional<TokenEntity> findByUser(UserEntity user);
}
