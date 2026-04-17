package org.bank.present.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.bank.core.services.UserService;
import org.bank.memory.DTO_entities.AccountDTO;
import org.bank.memory.DTO_entities.UserDTO;
import org.bank.memory.entities.users.HairColor;
import org.bank.memory.exceptions.UserExceptions;
import org.bank.memory.requestEntites.CreateUserRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
        private final UserService userService;

        @Operation(summary = "Регистрация нового пользователя")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован\n"),
                        @ApiResponse(responseCode = "400", description = "Пользователь с таким логином уже существует\n")
        })
        @PostMapping
        public ResponseEntity<UserDTO> registerUser(@RequestBody CreateUserRequest createUserRequest)
                        throws Exception {
                UserDTO userDTO = userService.registerUser(createUserRequest);
                return ResponseEntity.status(HttpStatus.CREATED).body(userDTO);
        }

        @Operation(summary = "Получить информацию о пользователе по логину")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Информация о пользователе\n"),
                        @ApiResponse(responseCode = "404", description = "Пользователь не найден\n")
        })
        @GetMapping("/{login}")
        public ResponseEntity<UserDTO> getUserInfo(@PathVariable("login") String login)
                        throws UserExceptions {
                UserDTO userDTO = userService.getUserInfo(login);
                return new ResponseEntity<>(userDTO, HttpStatus.OK);
        }

        @Operation(summary = "Добавить друга")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Друг успешно добавлен\n"),
                        @ApiResponse(responseCode = "400", description = "Ошибка при добавлении друга\n"),
                        @ApiResponse(responseCode = "404", description = "Пользователь или друг не найден\n")
        })
        @PostMapping("/{user_id}/add_friend/{friend_id}")
        public ResponseEntity<ArrayList<UserDTO>> addFriend(@PathVariable("user_id") @NonNull Long user_id,
                        @PathVariable("friend_id") @NonNull Long friend_id)
                        throws Exception {
                ArrayList<UserDTO> userDTOS = userService.addFriend(user_id, friend_id);
                return new ResponseEntity<>(userDTOS, HttpStatus.OK);
        }

        @Operation(summary = "Удалить друга у пользователя")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Друг успешно удалён\n"),
                        @ApiResponse(responseCode = "400", description = "Ошибка при удалении друга\n"),
                        @ApiResponse(responseCode = "404", description = "Пользователь или друг не найден\n")
        })
        @DeleteMapping("/{user_id}/delete_friend/{friend_id}")
        public ResponseEntity<ArrayList<UserDTO>> deleteFriend(@PathVariable("user_id") @NonNull Long user_id,
                        @PathVariable("friend_id") @NonNull Long friend_id)
                        throws UserExceptions {
                ArrayList<UserDTO> userDTOS = userService.deleteFriend(user_id, friend_id);
                return new ResponseEntity<>(userDTOS, HttpStatus.OK);
        }

        @Operation(summary = "Получить все счёта пользователя")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Счета пользователя\n"),
                        @ApiResponse(responseCode = "404", description = "Пользователь не найден\n")
        })
        @GetMapping("/{userid}/accounts")
        public ResponseEntity<List<AccountDTO>> getUserAccounts(@PathVariable("userid") @NonNull Long userid)
                        throws UserExceptions {
                List<AccountDTO> accounts = userService.getUserAccounts(userid);
                return new ResponseEntity<>(accounts, HttpStatus.OK);
        }

        @Operation(summary = "Получить всех друзей пользователя")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Друзья пользователя\n"),
                        @ApiResponse(responseCode = "404", description = "Пользователь не найден\n")
        })
        @GetMapping("/{userid}/friends")
        public ResponseEntity<List<Long>> getUserFriends(@PathVariable @NonNull Long userid)
                        throws UserExceptions {
                List<Long> friends = userService.getUserFriends(userid);
                return new ResponseEntity<>(friends, HttpStatus.OK);
        }

        @Operation(summary = "Получить всех пользователей")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Существующие пользователи: \n"),
                        @ApiResponse(responseCode = "404", description = "Пользователи не найдены\n")
        })
        @GetMapping
        public ResponseEntity<List<UserDTO>> getAllUsers() throws UserExceptions {
                List<UserDTO> users = userService.getAll();
                return new ResponseEntity<>(users, HttpStatus.OK);
        }

        @GetMapping("/{haircolor}/{gender}")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Пользователи с фильтрацией " +
                                        "по цвету волос и полу: \n"),
                        @ApiResponse(responseCode = "404", description = "Пользователи не найдены\n")
        })
        public ResponseEntity<List<UserDTO>> getAllUsersFilteredByHairColorAndGender(
                        @PathVariable("haircolor") String hairColor,
                        @PathVariable("gender") String gender) throws UserExceptions {
                HairColor hairColorEnum = HairColor.valueOf(hairColor.toUpperCase());
                List<UserDTO> userDTOs = userService.getFilteredUsers(hairColorEnum, gender);
                return new ResponseEntity<>(userDTOs, HttpStatus.OK);
        }

        @GetMapping("/{haircolor}/")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Пользователи с фильтрацией " +
                                        "по цвету волос: \n"),
                        @ApiResponse(responseCode = "404", description = "Пользователи не найдены\n")
        })
        public ResponseEntity<List<UserDTO>> getAllUsersFilteredByHairColor(
                        @PathVariable("haircolor") String haircolor) throws UserExceptions {
                HairColor hairColorEnum = HairColor.valueOf(haircolor.toUpperCase());
                List<UserDTO> users = userService.getFilteredUsersByHairColor(hairColorEnum);

                return new ResponseEntity<>(users, HttpStatus.OK);
        }

        @GetMapping("/{gender}/")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Пользователи с фильтрацией " +
                                        "по полу: \n"),
                        @ApiResponse(responseCode = "404", description = "Пользователи не найдены\n")
        })
        public ResponseEntity<List<UserDTO>> getAllUsersFilteredByGender(@PathVariable String gender)
                        throws UserExceptions {
                List<UserDTO> users = userService.getFilteredUsersByGender(gender);
                return new ResponseEntity<>(users, HttpStatus.OK);
        }
}
