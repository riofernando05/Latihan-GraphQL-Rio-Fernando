/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.graphqlproductcrud;

/**
 *
 * @author riofe
 */
import graphql.GraphQL;
import graphql.schema.idl.*;
import graphql.schema.*;
import java.io.*;
import java.util.Objects;

public class GraphQLConfig {
    public static GraphQL init() throws IOException {
        InputStream schemaStream = GraphQLConfig.class.getClassLoader().getResourceAsStream("schema.graphqls");
        if (schemaStream == null) throw new RuntimeException("schema.graphqls tidak ditemukan.");

        String schema = new String(schemaStream.readAllBytes());

        TypeDefinitionRegistry registry = new SchemaParser().parse(schema);

        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", builder -> builder
                .dataFetcher("allProducts", env -> ProductRepository.findAll())
                .dataFetcher("productById", env -> parseId(env.getArgument("id"), ProductRepository::findById))
            )
            .type("Mutation", builder -> builder
                .dataFetcher("addProduct", env -> ProductRepository.add(
                    env.getArgument("name"),
                    ((Number) env.getArgument("price")).doubleValue(),
                    env.getArgument("category")
                ))
                .dataFetcher("updateProduct", env -> {
                    Long id = parseIdValue(env.getArgument("id"));
                    return ProductRepository.update(
                        id,
                        env.getArgument("name"),
                        ((Number) env.getArgument("price")).doubleValue(),
                        env.getArgument("category")
                    );
                })
                .dataFetcher("deleteProduct", env -> {
                    Long id = parseIdValue(env.getArgument("id"));
                    return ProductRepository.delete(id);
                })
            )
            .build();

        GraphQLSchema schemaGraph = new SchemaGenerator().makeExecutableSchema(registry, wiring);
        return GraphQL.newGraphQL(schemaGraph).build();
    }

    private static Long parseIdValue(Object raw) {
        if (raw instanceof Number) return ((Number) raw).longValue();
        if (raw instanceof String) return Long.parseLong((String) raw);
        return null;
    }

    private static Product parseId(Object rawId, java.util.function.Function<Long, Product> fetcher) {
        Long id = parseIdValue(rawId);
        return id != null ? fetcher.apply(id) : null;
    }
}
