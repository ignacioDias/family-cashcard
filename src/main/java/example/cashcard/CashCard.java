package example.cashcard;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "cash_cards")
public class CashCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double amount;
    private String owner;

    // Default constructor for JPA
    public CashCard() {
    }

    public CashCard(Long id, Double amount, String owner) {
        this.id = id;
        this.amount = amount;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public Double getAmount() {
        return amount;
    }

    public String getOwner() {
        return owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CashCard cashCard = (CashCard) o;
        return Objects.equals(id, cashCard.id) &&
               Objects.equals(amount, cashCard.amount) &&
               Objects.equals(owner, cashCard.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, amount, owner);
    }
}

