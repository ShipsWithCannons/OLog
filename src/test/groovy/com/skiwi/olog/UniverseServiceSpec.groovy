package com.skiwi.olog

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(UniverseService)
@Mock([Universe, Server])
@TestMixin(GrailsUnitTestMixin)
class UniverseServiceSpec extends Specification {
    def serverCountryCode = "en"
    def server = new Server(countryCode: serverCountryCode)
    def universeId = 1
    def universeName = "1"

    static doWithSpring = {
        serverService(ServerService)
    }

    def setup() {
    }

    def cleanup() {
    }

    void "test get or create universe given server"() {
        when: "get non-existing universe"
        def createdUniverse = service.getOrCreateUniverse(server, universeId, universeName)

        then: "universe should be created"
        createdUniverse
        createdUniverse.server == server
        createdUniverse.universeId == universeId
        createdUniverse.name == universeName
        Universe.findByServerAndUniverseId(server, universeId) == createdUniverse

        when: "get existing universe"
        def existingUniverse = service.getOrCreateUniverse(server, universeId, universeName)

        then: "universe should exist"
        existingUniverse
        existingUniverse.server == server
        existingUniverse.universeId == universeId
        existingUniverse.name == universeName
        Universe.findByServerAndUniverseId(server, universeId) == existingUniverse
    }

    void "test get or create universe given server country code"() {
        when: "get non-existing universe"
        def createdUniverse = service.getOrCreateUniverse(serverCountryCode, universeId, universeName)

        then: "universe should be created"
        createdUniverse
        createdUniverse.server.countryCode == serverCountryCode
        createdUniverse.universeId == universeId
        createdUniverse.name == universeName
        Universe.createCriteria().get {
            server {
                eq "countryCode", serverCountryCode
            }
            eq "universeId", universeId
        } == createdUniverse

        when: "get existing universe"
        def existingUniverse = service.getOrCreateUniverse(serverCountryCode, universeId, universeName)

        then: "universe should exist"
        existingUniverse
        existingUniverse.server.countryCode == serverCountryCode
        existingUniverse.universeId == universeId
        existingUniverse.name == universeName
        Universe.createCriteria().get {
            server {
                eq "countryCode", serverCountryCode
            }
            eq "universeId", universeId
        } == existingUniverse
    }

    void "test get or create universe without name given server"() {
        when: "get non-existing universe"
        def createdUniverse = service.getOrCreateUniverse(server, universeId)

        then: "universe should be created"
        createdUniverse
        createdUniverse.server == server
        createdUniverse.universeId == universeId
        Universe.findByServerAndUniverseId(server, universeId) == createdUniverse

        when: "get existing universe"
        def existingUniverse = service.getOrCreateUniverse(server, universeId)

        then: "universe should exist"
        existingUniverse
        existingUniverse.server == server
        existingUniverse.universeId == universeId
        Universe.findByServerAndUniverseId(server, universeId) == existingUniverse
    }

    void "test get or create universe without name given server country code"() {
        when: "get non-existing universe"
        def createdUniverse = service.getOrCreateUniverse(serverCountryCode, universeId)

        then: "universe should be created"
        createdUniverse
        createdUniverse.server.countryCode == serverCountryCode
        createdUniverse.universeId == universeId
        Universe.createCriteria().get {
            server {
                eq "countryCode", serverCountryCode
            }
            eq "universeId", universeId
        } == createdUniverse

        when: "get existing universe"
        def existingUniverse = service.getOrCreateUniverse(serverCountryCode, universeId)

        then: "universe should exist"
        existingUniverse
        existingUniverse.server.countryCode == serverCountryCode
        existingUniverse.universeId == universeId
        Universe.createCriteria().get {
            server {
                eq "countryCode", serverCountryCode
            }
            eq "universeId", universeId
        } == existingUniverse
    }
}