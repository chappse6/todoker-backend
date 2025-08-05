package com.todoker.todokerbackend.service

import com.todoker.todokerbackend.domain.user.User
import com.todoker.todokerbackend.domain.user.UserPreference
import com.todoker.todokerbackend.domain.user.UserRole
import com.todoker.todokerbackend.exception.UserException
import com.todoker.todokerbackend.repository.UserPreferenceRepository
import com.todoker.todokerbackend.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val passwordEncoder: PasswordEncoder
) : UserDetailsService {
    
    override fun loadUserByUsername(username: String): UserDetails {
        return userRepository.findByUsernameValue(username)
            .orElseThrow { UsernameNotFoundException("User not found: $username") }
    }
    
    fun findById(id: Long): User {
        return userRepository.findById(id)
            .orElseThrow { NoSuchElementException("User not found with id: $id") }
    }
    
    fun findByUsername(username: String): User {
        return userRepository.findByUsernameValue(username)
            .orElseThrow { NoSuchElementException("User not found: $username") }
    }
    
    fun findByEmail(email: String): User {
        return userRepository.findByEmail(email)
            .orElseThrow { NoSuchElementException("User not found with email: $email") }
    }
    
    fun existsByUsername(username: String): Boolean {
        return userRepository.existsByUsernameValue(username)
    }
    
    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }
    
    @Transactional
    fun createUser(
        username: String,
        email: String,
        password: String,
        nickname: String? = null
    ): User {
        if (existsByUsername(username)) {
            throw UserException.usernameAlreadyExists(username)
        }
        if (existsByEmail(email)) {
            throw UserException.emailAlreadyExists(email)
        }
        
        val user = User(
            usernameValue = username,
            email = email,
            password = passwordEncoder.encode(password),
            nickname = nickname ?: username,
            role = UserRole.USER
        )
        
        val savedUser = userRepository.save(user)
        
        // Create default preferences
        val preference = UserPreference(user = savedUser)
        userPreferenceRepository.save(preference)
        savedUser.preference = preference
        
        return savedUser
    }
    
    @Transactional
    fun updateUser(
        userId: Long,
        nickname: String? = null,
        email: String? = null
    ): User {
        val user = findById(userId)
        
        email?.let {
            if (it != user.email && existsByEmail(it)) {
                throw UserException.emailAlreadyExists(it)
            }
        }
        
        user.updateProfile(nickname, email)
        return userRepository.save(user)
    }
    
    @Transactional
    fun changePassword(userId: Long, currentPassword: String, newPassword: String): User {
        val user = findById(userId)
        
        if (!passwordEncoder.matches(currentPassword, user.password)) {
            throw UserException.invalidPassword()
        }
        
        user.updatePassword(passwordEncoder.encode(newPassword))
        return userRepository.save(user)
    }
    
    @Transactional
    fun updatePassword(userId: Long, newPassword: String): User {
        val user = findById(userId)
        user.updatePassword(passwordEncoder.encode(newPassword))
        return userRepository.save(user)
    }
    
    @Transactional
    fun deleteUser(userId: Long) {
        val user = findById(userId)
        userRepository.delete(user)
    }
    
    fun getUserWithPreference(userId: Long): User {
        return userRepository.findByIdWithPreference(userId)
            .orElseThrow { NoSuchElementException("User not found with id: $userId") }
    }
    
    @Transactional
    fun updateUserPreference(
        userId: Long,
        themeBackground: String? = null,
        themeElement: String? = null,
        pomodoroWorkTime: Int? = null,
        pomodoroBreakTime: Int? = null,
        pomodoroLongBreakTime: Int? = null,
        pomodoroSessionsBeforeLongBreak: Int? = null,
        enableNotifications: Boolean? = null,
        enableSoundEffects: Boolean? = null,
        language: String? = null,
        timezone: String? = null,
        startOfWeek: Int? = null
    ): UserPreference {
        val preference = userPreferenceRepository.findByUserId(userId)
            .orElseThrow { NoSuchElementException("User preference not found for user: $userId") }
        
        preference.updateTheme(themeBackground, themeElement)
        preference.updatePomodoroSettings(
            pomodoroWorkTime,
            pomodoroBreakTime,
            pomodoroLongBreakTime,
            pomodoroSessionsBeforeLongBreak
        )
        preference.updateNotificationSettings(enableNotifications, enableSoundEffects)
        preference.updateLocalizationSettings(language, timezone, startOfWeek)
        
        return userPreferenceRepository.save(preference)
    }
}