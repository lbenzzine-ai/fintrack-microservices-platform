package com.fintrack.user.service;

import com.fintrack.user.dto.UpdateUserRequest;
import com.fintrack.user.dto.UserResponse;
import com.fintrack.user.entity.User;
import com.fintrack.user.exception.UserNotFoundException;
import com.fintrack.user.mapper.UserMapper;
import com.fintrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = "users:byId", key = "#uuid")
    public UserResponse findByUuid(String uuid) {
        User user = userRepository.findByUuidWithRoles(uuid).orElseThrow(() -> new UserNotFoundException(uuid));
        log.debug("DB hit for user uuid={}", uuid);
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional
    @Caching(put  = { @CachePut (value = "users:byId", key = "#uuid") },
             evict = { @CacheEvict(value = "users:byEmail", allEntries = true) })
    public UserResponse update(String uuid, UpdateUserRequest req) {
        User user = userRepository.findByUuidWithRoles(uuid).orElseThrow(() -> new UserNotFoundException(uuid));
        if (req.firstName() != null) user.setFirstName(req.firstName());
        if (req.lastName() != null)  user.setLastName(req.lastName());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    @CacheEvict(value = {"users:byId","users:byEmail"}, key = "#uuid")
    public void delete(String uuid) {
        User user = userRepository.findByUuidWithRoles(uuid).orElseThrow(() -> new UserNotFoundException(uuid));
        userRepository.delete(user);
        log.info("Deleted user uuid={}", uuid);
    }
}
