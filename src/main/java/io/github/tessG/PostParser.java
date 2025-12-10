package io.github.tessG;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

class PostsParser {
    public ArrayList<String> parsePosts() {
        ArrayList<String> posts = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader("data/dscposts.csv"))) {
           reader.readNext(); // Skip header

            String[] line;
            while ((line = reader.readNext()) != null) {
                String postNumber = line[0];
                String subject = line[1];
                String body = line[2];
                posts.add(subject+":"+body);

            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException | CsvValidationException e) {
            throw new RuntimeException(e);
        }
        return posts;
    }

}