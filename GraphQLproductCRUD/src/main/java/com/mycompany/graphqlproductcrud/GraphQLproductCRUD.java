/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.graphqlproductcrud;

/**
 *
 * @author riofe
 */
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import graphql.ExecutionResult;
import graphql.GraphQL;
import static spark.Spark.port;
import static spark.Spark.post;

public class GraphQLproductCRUD {
    public static void main(String[] args) throws Exception {
        GraphQL graphql = GraphQLConfig.init();
        Gson gson = new Gson();

        port(4567);

        post("/graphql", (req, res) -> {
            res.type("application/json");

            JsonObject body = gson.fromJson(req.body(), JsonObject.class);
            String query = body.get("query").getAsString();

            ExecutionResult result = graphql.execute(query);
            return gson.toJson(result.toSpecification());
        });
    }
}
