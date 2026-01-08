package com.axios.midaxio.repository;

import com.axios.midaxio.entity.IgnVerificationTask;
import com.axios.midaxio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IgnVerificationTaskRepository extends JpaRepository<IgnVerificationTask, Long> {
    Optional<IgnVerificationTask> findByUser(User user);

    void deleteByUser(User user);
}