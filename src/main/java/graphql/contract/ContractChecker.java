package graphql.contract;


import com.google.gson.Gson;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.TypeResolutionEnvironment;
import graphql.introspection.IntrospectionResultToSchema;
import graphql.language.Argument;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FieldDefinition;
import graphql.language.FragmentDefinition;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.Node;
import graphql.language.UnionTypeDefinition;
import graphql.parser.Parser;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.WiringFactory;
import graphql.validation.DocumentVisitor;
import graphql.validation.LanguageTraversal;
import graphql.validation.TraversalContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ContractChecker {

    public ContractCheckResult checkContract(String queryString, String oldSchemaString, String currentSchemaString) {
        Map currentSchemaJson = new Gson().fromJson(currentSchemaString, Map.class);
        Map oldSchemaJson = new Gson().fromJson(oldSchemaString, Map.class);

        IntrospectionResultToSchema introspectionResultToSchema = new IntrospectionResultToSchema();

        SchemaParser schemaParser = new SchemaParser();
        Document oldSchemaDefinition = introspectionResultToSchema.createSchemaDefinition(oldSchemaJson);
        TypeDefinitionRegistry oldTypeDefinitionRegistry = schemaParser.buildRegistry(oldSchemaDefinition);

        Document currentSchemaDefinition = introspectionResultToSchema.createSchemaDefinition(currentSchemaJson);
        TypeDefinitionRegistry currentTypeDefinitionRegistry = schemaParser.buildRegistry(currentSchemaDefinition);

        Parser parser = new Parser();
        Document query = parser.parseDocument(queryString);

        WiringFactory wiringFactory = new WiringFactory() {
            @Override
            public boolean providesTypeResolver(TypeDefinitionRegistry registry, InterfaceTypeDefinition interfaceType) {
                return true;
            }

            @Override
            public boolean providesTypeResolver(TypeDefinitionRegistry registry, UnionTypeDefinition unionType) {
                return true;
            }

            @Override
            public TypeResolver getTypeResolver(TypeDefinitionRegistry registry, InterfaceTypeDefinition interfaceType) {
                return env -> null;
            }

            @Override
            public TypeResolver getTypeResolver(TypeDefinitionRegistry registry, UnionTypeDefinition unionType) {
                return new TypeResolver() {
                    @Override
                    public GraphQLObjectType getType(TypeResolutionEnvironment env) {
                        return null;
                    }
                };
            }

            @Override
            public boolean providesDataFetcher(TypeDefinitionRegistry registry, FieldDefinition definition) {
                return false;
            }

            @Override
            public DataFetcher getDataFetcher(TypeDefinitionRegistry registry, FieldDefinition definition) {
                return null;
            }
        };

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring().wiringFactory(wiringFactory).build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema oldSchema = schemaGenerator.makeExecutableSchema(oldTypeDefinitionRegistry, runtimeWiring);
        GraphQLSchema currentSchema = schemaGenerator.makeExecutableSchema(currentTypeDefinitionRegistry, runtimeWiring);

        GraphQL oldGraphQL = GraphQL.newGraphQL(oldSchema).build();
        ExecutionResult oldResult = oldGraphQL.execute(queryString);

        GraphQL currentGraphQL = GraphQL.newGraphQL(currentSchema).build();
        ExecutionResult currentResult = oldGraphQL.execute(queryString);

        LanguageTraversal languageTraversal = new LanguageTraversal();
        TraversalContext traversalContextOld = new TraversalContext(oldSchema);
        TraversalContext traversalContextCurrent = new TraversalContext(currentSchema);

        CheckerVisitor checkerVisitor = new CheckerVisitor(traversalContextOld, traversalContextCurrent);
        languageTraversal.traverse(query, checkerVisitor);

        return new ContractCheckResult(checkerVisitor.errors);
    }


    static class CheckerVisitor implements DocumentVisitor {

        TraversalContext traversalContextOld;
        TraversalContext traversalContextCurrent;
        List<String> errors = new ArrayList<>();

        public CheckerVisitor(TraversalContext traversalContextOld, TraversalContext traversalContextCurrent) {
            this.traversalContextOld = traversalContextOld;
            this.traversalContextCurrent = traversalContextCurrent;
        }

        @Override
        public void enter(Node node, List<Node> path) {
            traversalContextOld.enter(node, path);
            traversalContextCurrent.enter(node, path);

            if (node instanceof Field) {
                checkField((Field) node);
            } else if (node instanceof Argument) {
                checkArgument((Argument) node);
            }
        }

        private void checkArgument(Argument argument) {
            GraphQLArgument oldArgument = traversalContextOld.getArgument();
            GraphQLArgument currentArgument = traversalContextCurrent.getArgument();

            String oldTypeName = oldArgument.getType().getName();
            String currentTypeName = currentArgument.getType().getName();
            if (!Objects.equals(oldTypeName, currentTypeName)) {
                errors.add(String.format("Different argument types for argument %s: %s vs %s", argument.getName(), oldTypeName, currentTypeName));
            }
        }


        private void checkField(Field field) {
            GraphQLOutputType outputTypeOld = traversalContextOld.getOutputType();
            GraphQLOutputType outputTypeCurrent = traversalContextCurrent.getOutputType();
            if (outputTypeOld == null || outputTypeCurrent == null) {
                errors.add(String.format("Unknown type for field %s", field));
                return;
            }
            if (!Objects.equals(outputTypeOld.getName(), outputTypeCurrent.getName())) {
                errors.add(String.format("Different types for field %s: %s vs %s", field.getName(), outputTypeOld.getName(), outputTypeCurrent.getName()));
            }
        }

        @Override
        public void leave(Node node, List<Node> path) {
            traversalContextOld.leave(node, path);
            traversalContextCurrent.leave(node, path);

        }

    }
}
