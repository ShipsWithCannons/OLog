package com.skiwi.olog

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.validation.ValidationException
import spock.lang.Specification

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(PlayerService)
@Mock([Player, PlayerAlias, Universe, Server, ServerService])
class PlayerServiceSpec extends Specification {
    def playerId = 103168
    def playerName = "skiwi"
    Universe universe
    Universe universe2
    Instant now

    def setup() {
        def universeService = mockService(UniverseService)
        universe = universeService.getOrCreateUniverse("en", 1, "1")
        universe2 = universeService.getOrCreateUniverse("en", 2, "2")
        now = Instant.now()
    }

    def cleanup() {
    }

    void "test get or create player"() {
        when: "get non-existing player"
        def createdPlayer = service.getOrCreatePlayer(universe, playerId, playerName)

        then: "player should be created"
        createdPlayer
        createdPlayer.universe == universe
        createdPlayer.playerId == playerId
        createdPlayer.currentName == playerName
        Player.findByUniverseAndPlayerId(universe, playerId) == createdPlayer

        when: "get existing player"
        def existingPlayer = service.getOrCreatePlayer(universe, playerId, playerName)

        then: "player should exist"
        existingPlayer
        existingPlayer.universe == universe
        existingPlayer.playerId == playerId
        existingPlayer.currentName == playerName
        Player.findByUniverseAndPlayerId(universe, playerId) == existingPlayer
    }

    void "test create player"() {
        when: "create new player"
        def createdPlayer = service.createPlayer(universe, playerId, playerName)

        then: "player should be created"
        createdPlayer
        createdPlayer.universe == universe
        createdPlayer.playerId == playerId
        createdPlayer.currentName == playerName
        Player.findByUniverseAndPlayerId(universe, playerId) == createdPlayer

        when: "create other player with same playerId in same universe"
        service.createPlayer(universe, playerId, playerName)

        then: "validation error should be thrown"
        def ex = thrown(ValidationException)
        ex.errors.allErrors.stream().flatMap({objectError -> objectError.codes.toList().stream()}).any { it == "unique" }

        when: "create other player with same playerId in different universe"
        def otherPlayer = service.createPlayer(universe2, playerId, playerName)

        then: "that player should be created"
        otherPlayer
        otherPlayer.universe == universe2
        otherPlayer.playerId == playerId
        otherPlayer.currentName == playerName
        Player.findByUniverseAndPlayerId(universe2, playerId) == otherPlayer
    }

    void "test update player name and get player alias"() {
        given: "a player"
        def player = service.createPlayer(universe, playerId, playerName)
        def newPlayerName = "skiwi2"

        when: "player has been seen with same name"
        service.updatePlayerName(player, playerName, now)

        then: "player does not have a new alias"
        player.aliases.size() == 1
        player.getNameAt(now.minus(1, ChronoUnit.HOURS)) == playerName
        player.getNameAt(now) == playerName
        player.getNameAt(now.plus(1, ChronoUnit.HOURS)) == playerName

        when: "player has been seen with a different name"
        def updateInstant = now.minus(2, ChronoUnit.HOURS)
        service.updatePlayerName(player, newPlayerName, updateInstant)

        then: "player has a new alias"
        player.aliases.size() == 2
        service.getPlayerAlias(player, now.minus(4, ChronoUnit.HOURS)).name == playerName
        service.getPlayerAlias(player, now.minus(2, ChronoUnit.HOURS)).name == newPlayerName
        service.getPlayerAlias(player, now.plus(2, ChronoUnit.HOURS)).name == newPlayerName
        service.getPlayerAlias(player, now.minus(4, ChronoUnit.HOURS)).end == updateInstant
        service.getPlayerAlias(player, now.minus(2, ChronoUnit.HOURS)).begin == updateInstant
    }
}
