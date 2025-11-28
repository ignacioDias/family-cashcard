package example.cashcard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/cashcards")
public class CashCardController {

    private final CashCardRepository cashCardRepository;

    public CashCardController(CashCardRepository cashCardRepository) {
        this.cashCardRepository = cashCardRepository;
    }

    @GetMapping("/{requestedId}")
    private ResponseEntity<CashCard> findById(@PathVariable Long requestedId, Principal principal) {
        if(cashCardRepository.existsByIdAndOwner(requestedId, principal.getName())) {
            var cashCard = cashCardRepository.findByIdAndOwner(requestedId, principal.getName());
            return ResponseEntity.ok(cashCard);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    private ResponseEntity<List<CashCard>> findAllByOwner(Pageable pageable, Principal principal) {
        Page<CashCard> page = cashCardRepository.findByOwner(principal.getName(),
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))
                ));
        return ResponseEntity.ok(page.getContent());
    }

    @PostMapping
    private ResponseEntity<Void> createCashCard(@RequestBody CashCard cashCard, UriComponentsBuilder ucb, Principal principal) {
        var newCashCard = new CashCard(null, cashCard.getAmount(), principal.getName());
        cashCardRepository.save(newCashCard);
        URI locationOfNewCashCard = ucb
                .path("/cashcards/{id}")
                .buildAndExpand(newCashCard.getId())
                .toUri();
        return ResponseEntity.created(locationOfNewCashCard).build();
    }

    @PutMapping("/{requestedId}")
    private ResponseEntity<Void> updateCashCard(@PathVariable Long requestedId, @RequestBody CashCard cashCard, Principal principal) {
        if(cashCardRepository.existsByIdAndOwner(requestedId, principal.getName())) {
            var newCashCard = new CashCard(requestedId, cashCard.getAmount(), principal.getName());
            cashCardRepository.save(newCashCard);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{requestedId}")
    private ResponseEntity<Void> deleteCashCard(@PathVariable Long requestedId, Principal principal) {
        if(cashCardRepository.existsByIdAndOwner(requestedId, principal.getName())) {
            cashCardRepository.deleteById(requestedId);
            return ResponseEntity.noContent().build();
        }
        return  ResponseEntity.notFound().build();
    }
}
