package com.todoker.todokerbackend.repository

import com.todoker.todokerbackend.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, Long> {
    
    fun findByUsernameValue(username: String): Optional<User>
    
    fun findByEmail(email: String): Optional<User>
    
    fun existsByUsernameValue(username: String): Boolean
    
    fun existsByEmail(email: String): Boolean
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.preference WHERE u.usernameValue = :username")
    fun findByUsernameWithPreference(@Param("username") username: String): Optional<User>
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.preference WHERE u.id = :id")
    fun findByIdWithPreference(@Param("id") id: Long): Optional<User>
    
    @Query("SELECT u FROM User u WHERE u.enabledValue = true AND u.role = 'USER'")
    fun findAllActiveUsers(): List<User>
}