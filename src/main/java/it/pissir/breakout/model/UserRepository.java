package it.pissir.breakout.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

// Repositori JPA normal i corrent per a la taula d'usuaris
@Repository
public interface UserRepository extends CrudRepository<User, String> {
}
