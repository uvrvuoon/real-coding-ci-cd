package com.example.db2026termproject.repository;

import com.example.db2026termproject.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    Customer findByCnoAndPasswd(String cno, String passwd);
}
