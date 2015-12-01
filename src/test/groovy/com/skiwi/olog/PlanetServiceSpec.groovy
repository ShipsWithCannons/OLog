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
@TestFor(PlanetService)
@Mock([Planet, ServerGroup, Universe, Player, Coordinate, CoordinateService, PlanetLocation, PlanetAlias, ServerGroup, ServerGroupService, PlayerAlias])
class PlanetServiceSpec extends Specification {
    Universe universe
    Universe universe2
    Player player
    Player player2
    def planetId = 1000
    def galaxy = 2
    def solarSystem = 122
    def position = 12
    def planetName = "Homeworld"
    Instant now

    CoordinateService coordinateService

    def setup() {
        def universeService = mockService(UniverseService)
        def playerService = mockService(PlayerService)
        universe = universeService.getOrCreateUniverse("en", 135)
        universe2 = universeService.getOrCreateUniverse("en", 136)
        player = playerService.createPlayer(universe, 103168, "skiwi")
        player2 = playerService.createPlayer(universe2, 103168, "skiwi2")
        now = Instant.now()
        coordinateService = mockService(CoordinateService)
    }

    def cleanup() {
    }

    void "test find planet"() {
        when: "planet does not exist"
        def nonExistingPlanet = service.findPlanet(universe, planetId)

        then: "null should be returned"
        !nonExistingPlanet

        when: "planet gets created"
        service.createPlanet(player, planetId, galaxy, solarSystem, position, planetName)
        def planet = service.findPlanet(universe, planetId)

        then: "planet should be returned"
        planet
        planet.player == player
        planet.planetId == planetId
    }

    void "test create planet"() {
        when: "create new planet"
        def createdPlanet = service.createPlanet(player, planetId, galaxy, solarSystem, position, planetName)

        then: "planet should be created"
        createdPlanet
        createdPlanet.player == player
        createdPlanet.planetId == planetId
        createdPlanet.currentCoordinate.galaxy == galaxy
        createdPlanet.currentCoordinate.solarSystem == solarSystem
        createdPlanet.currentCoordinate.position == position
        createdPlanet.currentName == planetName
        Planet.findByPlayerAndPlanetId(player, planetId) == createdPlanet

        when: "create other planet with same planetId in same universe"
        service.createPlanet(player, planetId, galaxy, solarSystem, position, planetName)

        then: "validation error should be thrown"
        def ex = thrown(ValidationException)
        ex.errors.allErrors.stream().flatMap({objectError -> objectError.codes.toList().stream()}).any { it == "unique" }

        when: "create other planet with same planetId in different universe"
        def otherPlanet = service.createPlanet(player2, planetId, galaxy, solarSystem, position, planetName)

        then: "that planet should be created"
        otherPlanet
        otherPlanet.player == player2
        otherPlanet.planetId == planetId
        otherPlanet.currentCoordinate.galaxy == galaxy
        otherPlanet.currentCoordinate.solarSystem == solarSystem
        otherPlanet.currentCoordinate.position == position
        otherPlanet.currentName == planetName
        Planet.findByPlayerAndPlanetId(player2, planetId) == otherPlanet
    }

    void "test store planet location and get planet location"() {
        given: "a planet"
        def planet = service.createPlanet(player, planetId, galaxy, solarSystem, position, planetName)
        def planetCoordinate = planet.currentCoordinate
        def newPlanetCoordinate = coordinateService.getCoordinate(universe, galaxy, solarSystem + 1, position)

        when: "planet has been seen with same location"
        service.storePlanetLocation(planet, galaxy, solarSystem, position, now)

        then: "planet does not have a new location"
        planet.locations.size() == 1
        planet.getCoordinateAt(now.minus(1, ChronoUnit.HOURS)) == planetCoordinate
        planet.getCoordinateAt(now) == planetCoordinate
        planet.getCoordinateAt(now.plus(1, ChronoUnit.HOURS)) == planetCoordinate

        when: "planet has been seen with a different name"
        def updateInstant = now.minus(2, ChronoUnit.HOURS)
        service.storePlanetLocation(planet, galaxy, solarSystem + 1, position, updateInstant)

        then: "planet has a new alias"
        planet.locations.size() == 2
        service.getPlanetLocation(planet, now.minus(4, ChronoUnit.HOURS)).coordinate == planetCoordinate
        service.getPlanetLocation(planet, now.minus(2, ChronoUnit.HOURS)).coordinate == newPlanetCoordinate
        service.getPlanetLocation(planet, now.plus(2, ChronoUnit.HOURS)).coordinate == newPlanetCoordinate
        service.getPlanetLocation(planet, now.minus(4, ChronoUnit.HOURS)).end == updateInstant
        service.getPlanetLocation(planet, now.minus(2, ChronoUnit.HOURS)).begin == updateInstant
    }

    void "test store planet name and get planet alias"() {
        given: "a planet"
        def planet = service.createPlanet(player, planetId, galaxy, solarSystem, position, planetName)
        def newPlanetName = "Homeworld2"

        when: "planet has been seen with same name"
        service.storePlanetName(planet, planetName, now)

        then: "planet does not have a new alias"
        planet.aliases.size() == 1
        planet.getNameAt(now.minus(1, ChronoUnit.HOURS)) == planetName
        planet.getNameAt(now) == planetName
        planet.getNameAt(now.plus(1, ChronoUnit.HOURS)) == planetName

        when: "planet has been seen with a different name"
        def updateInstant = now.minus(2, ChronoUnit.HOURS)
        service.storePlanetName(planet, newPlanetName, updateInstant)

        then: "planet has a new alias"
        planet.aliases.size() == 2
        service.getPlanetAlias(planet, now.minus(4, ChronoUnit.HOURS)).name == planetName
        service.getPlanetAlias(planet, now.minus(2, ChronoUnit.HOURS)).name == newPlanetName
        service.getPlanetAlias(planet, now.plus(2, ChronoUnit.HOURS)).name == newPlanetName
        service.getPlanetAlias(planet, now.minus(4, ChronoUnit.HOURS)).end == updateInstant
        service.getPlanetAlias(planet, now.minus(2, ChronoUnit.HOURS)).begin == updateInstant
    }
}