package il.ac.jmc.matchday.repository;

import il.ac.jmc.matchday.entity.League;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link League} entities.
 */
@Repository
public interface LeagueRepository extends JpaRepository<League, Long> {

    /**
     * Used by the uniqueness check on create: true when a league already has this name.
     *
     * @param name the name to check
     * @return true if a league exists with the given name, false otherwise
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Used by the uniqueness check on edit: true when another league already has this name.
     *
     * @param name the name to check
     * @param id   the ID of the league to exclude
     * @return true if another league exists with the given name, false otherwise
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Returns all leagues sorted alphabetically for dropdowns and the leagues list.
     *
     * @return a list of all leagues ordered by name ascending
     */
    List<League> findAllByOrderByNameAsc();

    /**
     * Returns the alphabetically first league, used as the default league-table selection.
     *
     * @return an Optional containing the first league, or empty if no leagues exist
     */
    Optional<League> findTopByOrderByNameAsc();

    /**
     * Counts leagues without a logo URL, shown next to the admin Wikipedia-fetch button.
     *
     * @return the number of leagues missing logo URLs
     */
    @Query("""
            SELECT COUNT(l)
            FROM League l
            WHERE l.logoUrl IS NULL OR TRIM(l.logoUrl) = ''
            """)
    long countMissingLogoUrls();

    /**
     * Returns a single page of leagues ordered by id. Use with {@code PageRequest.of(offset, 1)} for random pick.
     *
     * @param pageable pagination details
     * @return a page of leagues
     */
    @Query("SELECT l FROM League l ORDER BY l.id ASC")
    List<League> findPage(Pageable pageable);

    /**
     * Filters leagues by optional name/description text, returning one page sorted alphabetically.
     * Pass {@code nameFilterEmpty = true} and an empty string for {@code name} to return all leagues.
     *
     * @param name            the search term for name or description
     * @param nameFilterEmpty true to ignore the name filter
     * @param pageable        pagination details
     * @return a page of matched leagues
     */
    @Query(value = """
            SELECT l FROM League l
            WHERE (:nameFilterEmpty = TRUE
                   OR LOWER(l.name) LIKE LOWER(CONCAT('%', :name, '%'))
                   OR LOWER(l.description) LIKE LOWER(CONCAT('%', :name, '%')))
            ORDER BY l.name ASC
            """,
           countQuery = """
            SELECT COUNT(l) FROM League l
            WHERE (:nameFilterEmpty = TRUE
                   OR LOWER(l.name) LIKE LOWER(CONCAT('%', :name, '%'))
                   OR LOWER(l.description) LIKE LOWER(CONCAT('%', :name, '%')))
            """)
    Page<League> findBySearchPageable(@Param("name")            String name,
                                       @Param("nameFilterEmpty") boolean nameFilterEmpty,
                                       Pageable pageable);
}
