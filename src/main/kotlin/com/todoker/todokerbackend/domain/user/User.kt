package com.todoker.todokerbackend.domain.user

import com.todoker.todokerbackend.domain.category.Category
import com.todoker.todokerbackend.domain.common.BaseEntity
import com.todoker.todokerbackend.domain.pomodoro.PomodoroSession
import com.todoker.todokerbackend.domain.todo.Todo
import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_user_email", columnList = "email", unique = true),
        Index(name = "idx_user_username", columnList = "username", unique = true)
    ]
)
class User(
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private val usernameValue: String,
    
    @Column(unique = true, nullable = false, length = 100)
    var email: String,
    
    @Column(nullable = false)
    private var password: String,
    
    @Column(nullable = false, length = 50)
    var nickname: String = usernameValue,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: UserRole = UserRole.USER,
    
    @Column(name = "is_enabled", nullable = false)
    private var enabledValue: Boolean = true
) : BaseEntity(), UserDetails {
    
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val todos: MutableSet<Todo> = mutableSetOf()
    
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val categories: MutableSet<Category> = mutableSetOf()
    
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val pomodoroSessions: MutableSet<PomodoroSession> = mutableSetOf()
    
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val refreshTokens: MutableSet<RefreshToken> = mutableSetOf()
    
    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var preference: UserPreference? = null
    
    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
    }
    
    override fun getPassword(): String = password
    
    override fun getUsername(): String = usernameValue
    
    override fun isAccountNonExpired(): Boolean = true
    
    override fun isAccountNonLocked(): Boolean = true
    
    override fun isCredentialsNonExpired(): Boolean = true
    
    override fun isEnabled(): Boolean = enabledValue
    
    fun updatePassword(newPassword: String) {
        this.password = newPassword
    }
    
    fun updateProfile(nickname: String? = null, email: String? = null) {
        nickname?.let { this.nickname = it }
        email?.let { this.email = it }
    }
    
    fun enable() {
        this.enabledValue = true
    }
    
    fun disable() {
        this.enabledValue = false
    }
}

enum class UserRole {
    USER,
    ADMIN
}