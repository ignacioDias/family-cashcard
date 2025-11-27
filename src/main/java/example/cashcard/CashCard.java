package example.cashcard;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;

record CashCard(@Id Long id, Double amount, String owner) {
}

