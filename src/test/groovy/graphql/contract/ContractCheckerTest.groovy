package graphql.contract

import spock.lang.Specification

class ContractCheckerTest extends Specification {


    def "no errors"() {
        given:
        String simpsons = this.getClass().getResource('/simpsons-introspection-old.json').text

        ContractChecker contractChecker = new ContractChecker()

        when:
        def result = contractChecker.checkContract("{character(firstName: \"Homer\") { id, firstName, lastName}}", simpsons, simpsons)
        then:
        result.result == true
        result.compatibilityErrors.size() == 0
        result.validationErrors.size() == 0

    }

    def "incompatible types: firstName changed "() {
        given:
        String simpsonsOld = this.getClass().getResource('/simpsons-introspection-old.json').text
        String simpsonsCurrent = this.getClass().getResource('/simpsons-introspection-current.json').text

        ContractChecker contractChecker = new ContractChecker()

        when:
        def result = contractChecker.checkContract("{character(firstName: \"Homer\") { id, firstName, lastName}}", simpsonsOld, simpsonsCurrent)
        then:
        result.result == false
        result.validationErrors.size() == 0
        result.compatibilityErrors == ["Different types for field firstName: String vs Int"]
    }

    def "incompatible types with fragment: firstName changed"() {
        String simpsonsOld = this.getClass().getResource('/simpsons-introspection-old.json').text
        String simpsonsCurrent = this.getClass().getResource('/simpsons-introspection-current.json').text

        ContractChecker contractChecker = new ContractChecker()

        def query = """ {
            character(firstName: "Homer") { 
                id, 
                ...MyFragment
                }
             }
             
           fragment MyFragment on Character {
               firstName,
               lastName 
           }
        """

        when:
        def result = contractChecker.checkContract(query, simpsonsOld, simpsonsCurrent)

        then:
        result.result == false
        result.validationErrors.size() == 0
        result.compatibilityErrors == ["Different types for field firstName: String vs Int"]

    }

    def "incompatible types with inline fragment: firstName changed"() {
        String simpsonsOld = this.getClass().getResource('/simpsons-introspection-old.json').text
        String simpsonsCurrent = this.getClass().getResource('/simpsons-introspection-current.json').text

        ContractChecker contractChecker = new ContractChecker()

        def query = """ {
            character(firstName: "Homer") { 
                id, 
                ...on Character {
                   firstName,
                   lastName 
                    }
                }
             }
        """

        when:
        def result = contractChecker.checkContract(query, simpsonsOld, simpsonsCurrent)

        then:
        result.result == false
        result.validationErrors.size() == 0
        result.compatibilityErrors == ["Different types for field firstName: String vs Int"]

    }

    def "invalid query: not a field"() {
        String simpsonsOld = this.getClass().getResource('/simpsons-introspection-old.json').text
        String simpsonsCurrent = this.getClass().getResource('/simpsons-introspection-current.json').text

        ContractChecker contractChecker = new ContractChecker()

        def query = """ {
            character(firstName: "Homer") { 
                id, 
                notAField
             }
           }
        """

        when:
        def result = contractChecker.checkContract(query, simpsonsOld, simpsonsCurrent)

        then:
        result.result == false
        result.validationErrors.size() == 1
        result.compatibilityErrors.size() == 0

    }

    def "invalid query: argument type invalid"() {
        String simpsonsOld = this.getClass().getResource('/simpsons-introspection-old.json').text
        String simpsonsCurrent = this.getClass().getResource('/simpsons-introspection-changed-argument.json').text

        ContractChecker contractChecker = new ContractChecker()

        def query = """ {
            character(firstName: "Homer") { 
                id
             }
           }
        """

        when:
        def result = contractChecker.checkContract(query, simpsonsOld, simpsonsCurrent)

        then:
        result.result == false
        result.validationErrors.size() == 1
        result.compatibilityErrors.size() == 0
    }

    def "invalid query: invalid syntax"() {
        String simpsonsOld = this.getClass().getResource('/simpsons-introspection-old.json').text
        String simpsonsCurrent = this.getClass().getResource('/simpsons-introspection-current.json').text

        ContractChecker contractChecker = new ContractChecker()

        def query = """ {
            character(firstName: "Homer") { 
                id
             }
          # }  => Syntax error
        """

        when:
        def result = contractChecker.checkContract(query, simpsonsOld, simpsonsCurrent)

        then:
        result.result == false
        result.invalidSyntaxError != null

    }
}
