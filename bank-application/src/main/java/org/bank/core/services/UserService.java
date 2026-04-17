package org.bank.core.services;

import lombok.RequiredArgsConstructor;
import org.bank.memory.DTO_entities.ClientEventDto;
import org.bank.memory.DTO_entities.AccountDTO;
import org.bank.memory.DTO_entities.UserDTO;
import org.bank.memory.entities.users.HairColor;
import org.bank.memory.entities.users.User;
import org.bank.memory.exceptions.UserExceptions;
import org.bank.core.mappers.AccountMapper;
import org.bank.core.mappers.UserMapper;
import org.bank.memory.repos.UserRepository;
import org.bank.memory.requestEntites.CreateUserRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Класс для работы с пользователями.
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AccountMapper accountMapper;
    private final KafkaEventProducer kafkaProducer;

    /**
     * Метод для регистрации пользователя.
     *
     * @param createUserRequest данные пользователя
     * @throws UserExceptions исключение, если пользователь уже существует
     */
    @Transactional
    public UserDTO registerUser(CreateUserRequest createUserRequest) throws Exception {
        HairColor hairColorEnum = HairColor.valueOf(createUserRequest.getHairColor().toUpperCase());
        User user = new User(createUserRequest.getLogin(), createUserRequest.getName(),
                createUserRequest.getAge(), createUserRequest.getGender(), hairColorEnum);
        if (userRepository.existsByLogin(user.getLogin())) {
            throw UserExceptions.UserAlreadyExistsException("Пользователь с таким логином уже существует.");
        }

        ClientEventDto event = ClientEventDto.fromUser(user, "CREATED");
        kafkaProducer.sendClientEvent(event);
        userRepository.save(user);
        return userMapper.toUserDTO(user);
    }

    /**
     * Метод для получения информации о пользователе.
     *
     * @param login логин пользователя
     */
    @Transactional(readOnly = true)
    public UserDTO getUserInfo(String login) throws UserExceptions {
        UserDTO userDTO = userMapper.toUserDTO(userRepository.findByLogin(login));
        if (userDTO == null) {
            throw UserExceptions.UserNotFoundException("Пользователь не найден.");
        }
        return userDTO;
    }

    /**
     * Метод для добавления друга.
     *
     * @param user_id   id пользователя
     * @param friend_id id друга
     */
    @Transactional
    public ArrayList<UserDTO> addFriend(@NonNull Long user_id, @NonNull Long friend_id) throws Exception {
        User user = userRepository.findById(user_id).orElse(null);
        User friend = userRepository.findById(friend_id).orElse(null);

        if (user == null || friend == null) {
            throw UserExceptions.UserNotFoundException("Пользователь или друг не найден.");
        }
        if (user.getFriends().contains(friend)) {
            throw UserExceptions.UserAlreadyExistsException("Друг уже добавлен.");
        }
        user.addFriend(friend);
        friend.addFriend(user);

        ClientEventDto userEvent = ClientEventDto.fromUser(user, "UPDATED");
        userEvent.setChanges(new ClientEventDto.FieldChanges(
                "friends",
                user.getFriends().size() - 1,
                user.getFriends().size()));

        ClientEventDto friendEvent = ClientEventDto.fromUser(friend, "UPDATED");
        friendEvent.setChanges(new ClientEventDto.FieldChanges(
                "friends",
                friend.getFriends().size() - 1,
                friend.getFriends().size()));

        kafkaProducer.sendClientEvent(userEvent);
        kafkaProducer.sendClientEvent(friendEvent);

        return new ArrayList<>(List.of(userMapper.toUserDTO(user), userMapper.toUserDTO(friend)));
    }

    /**
     * Метод для удаления друга.
     *
     * @param user_id   id пользователя
     * @param friend_id id друга
     */
    @Transactional
    public ArrayList<UserDTO> deleteFriend(@NonNull Long user_id, @NonNull Long friend_id) throws UserExceptions {
        User user = userRepository.findById(user_id).orElse(null);
        User friend = userRepository.findById(friend_id).orElse(null);
        if (user == null || friend == null) {
            throw UserExceptions.UserNotFoundException("Пользователь или друг не найден.");
        }
        if (!user.getFriends().contains(friend)) {
            throw UserExceptions.UserNotFoundException("Друг не найден.");
        }
        user.removeFriend(friend);
        friend.removeFriend(user);

        ClientEventDto userEvent = ClientEventDto.fromUser(user, "UPDATED");
        userEvent.setChanges(new ClientEventDto.FieldChanges(
                "friends",
                user.getFriends().size() + 1,
                user.getFriends().size()));

        ClientEventDto friendEvent = ClientEventDto.fromUser(friend, "UPDATED");
        friendEvent.setChanges(new ClientEventDto.FieldChanges(
                "friends",
                user.getFriends().size() + 1,
                friend.getFriends().size()));

        return new ArrayList<>(List.of(userMapper.toUserDTO(user), userMapper.toUserDTO(friend)));
    }

    /**
     * Метод для получения счета пользователя по логину.
     *
     * @param userid логин пользователя
     * @return счет пользователя
     */
    @Transactional(readOnly = true)
    public List<AccountDTO> getUserAccounts(@NonNull Long userid) throws UserExceptions {
        Optional<User> user = userRepository.findById(userid);
        if (user.isEmpty()) {
            throw UserExceptions.UserNotFoundException("Пользователь не найден.");
        }
        return accountMapper.toAccountDTOs(user.get().getAccounts());
    }

    /**
     * Метод для получения всех друзей пользователя.
     *
     * @param userid логин пользователя
     * @return список друзей пользователя
     */
    @Transactional(readOnly = true)
    public List<Long> getUserFriends(@NonNull Long userid) throws UserExceptions {
        Optional<User> user = userRepository.findById(userid);
        if (user.isEmpty()) {
            throw UserExceptions.UserNotFoundException("Пользователь не найден.");
        }
        UserDTO userDTO = userMapper.toUserDTO(user.get());
        return userDTO.getFriends();
    }

    /**
     * Метод для получения всех пользователей.
     *
     * @return список пользователей
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAll() throws UserExceptions {
        List<User> user = userRepository.findAll();
        if (user.isEmpty()) {
            throw UserExceptions.UserNotFoundException("Пользователь не найден.");
        }
        return userMapper.toUserDTOs(user);
    }

    /**
     * Метод для получения всех пользователей с фильтрацией по цвету волос и полу.
     *
     * @param hairColor цвет волос
     * @param gender    пол
     * @return список пользователей
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getFilteredUsers(HairColor hairColor, String gender) throws UserExceptions {
        List<User> users = userRepository.findByHairColorAndGender(hairColor, gender);
        if (users.isEmpty()) {
            throw UserExceptions.UserNotFoundException("Пользователи не найдены.");
        }
        return userMapper.toUserDTOs(users);
    }

    /**
     * Метод для получения всех пользователей с фильтрацией по цвету волос.
     *
     * @param hairColor цвет волос
     * @return список пользователей
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getFilteredUsersByHairColor(HairColor hairColor) throws UserExceptions {
        List<User> users = userRepository.findByHairColor(hairColor);
        if (users.isEmpty()) {
            throw UserExceptions.UserNotFoundException("Пользователи не найдены.");
        }
        return userMapper.toUserDTOs(users);
    }

    /**
     * Метод для получения всех пользователей с фильтрацией по полу.
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getFilteredUsersByGender(String gender) throws UserExceptions {
        List<User> users = userRepository.findByGender(gender);
        if (users.isEmpty()) {
            throw UserExceptions.UserNotFoundException("Пользователи не найдены.");
        }
        return userMapper.toUserDTOs(users);
    }
}