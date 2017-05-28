package graphql.contract

import spock.lang.Specification

class ContractCheckerTest extends Specification {


    def "test 1"() {
        given:
        String simpsonsOld = this.getClass().getResource('/simpsons-introspection-old.json').text
        String simpsonsCurrent = this.getClass().getResource('/simpsons-introspection-current.json').text

        ContractChecker contractChecker = new ContractChecker()

        when:
        def result = contractChecker.checkContract("{character(firstName: \"Homer\") { id, firstName, lastName}}", simpsonsOld, simpsonsCurrent)
        println result.errors
        then:
        result.errors.size() == 2
    }

    def "test 2"() {
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
        println result.errors
        then:
        result.errors.size() == 2

    }

    def "test 3"() {
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
        println result.errors
        then:
        result.errors.size() == 2

    }
}
