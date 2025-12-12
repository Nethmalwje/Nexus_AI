package com.Nexus.Chatter.util;



import com.pgvector.PGvector;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Converter(autoApply = true)
public class VectorConverter implements AttributeConverter<List<Double>, String> {

    @Override
    public String convertToDatabaseColumn(List<Double> attribute) {
        if (attribute == null) return null;
        // Convert List<Double> -> float[] -> PGvector object -> String
        float[] floatArray = new float[attribute.size()];
        for (int i = 0; i < attribute.size(); i++) {
            floatArray[i] = attribute.get(i).floatValue();
        }
        return new PGvector(floatArray).toString();
    }

    @Override
    public List<Double> convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        // Convert String -> PGvector object -> float[] -> List<Double>
        PGvector pgVector = null;
        try {
            pgVector = new PGvector(dbData);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        float[] floatArray = pgVector.toArray();

        // Convert float[] back to List<Double>
        java.util.ArrayList<Double> list = new java.util.ArrayList<>();
        for (float f : floatArray) {
            list.add((double) f);
        }
        return list;
    }
}