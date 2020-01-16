package ru.shemplo.wtc.logic;

import lombok.*;

import java.nio.file.Path;
import java.util.List;

@ToString
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    
    private String name, description;
    
    private Path location;
    
    private Long time;
    
    private List <String> ignore;
    
}
