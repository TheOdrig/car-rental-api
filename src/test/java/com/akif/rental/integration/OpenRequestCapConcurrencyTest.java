package com.akif.rental.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.rental.api.RentalRequest;
import com.akif.rental.api.RentalService;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.exception.OpenRentalRequestLimitException;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.scheduler.ReminderScheduler;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Open REQUESTED cap under concurrency")
class OpenRequestCapConcurrencyTest {

    private static final int PARALLEL_REQUESTS = 8;

    @Autowired
    private RentalService rentalService;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RentalProperties properties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ReminderScheduler reminderScheduler;

    private User first;
    private User second;
    private List<Car> cars;

    @BeforeEach
    void setUp() {
        cleanUp();
        first = userRepository.save(TestDataBuilder.createTestUser("cap-user-one"));
        second = userRepository.save(TestDataBuilder.createTestUser("cap-user-two"));
        cars = new ArrayList<>();
        for (int i = 0; i < PARALLEL_REQUESTS * 2; i++) {
            cars.add(carRepository.save(TestDataBuilder.createAvailableCar()));
        }
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM car_rental.event_publication");
        jdbcTemplate.update("DELETE FROM car_rental.payments");
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
    }

    private RentalRequest request(Car car) {
        LocalDate start = LocalDate.now().plusDays(10);
        return TestDataBuilder.createRentalRequest(car.getId(), start, start.plusDays(3));
    }

    private List<Throwable> runParallel(List<Callable<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Throwable>> futures = new ArrayList<>();
        for (Callable<Object> task : tasks) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    task.call();
                    return null;
                } catch (Throwable error) {
                    return error;
                }
            }));
        }
        ready.await();
        go.countDown();
        List<Throwable> errors = new ArrayList<>();
        for (Future<Throwable> future : futures) {
            errors.add(future.get());
        }
        pool.shutdownNow();
        return errors;
    }

    @Test
    @DisplayName("lets exactly the configured number of simultaneous requests through for one user")
    void parallelRequestsRespectTheCap() throws Exception {
        int cap = properties.maxOpenRequestsPerUser();
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < PARALLEL_REQUESTS; i++) {
            Car car = cars.get(i);
            tasks.add(() -> rentalService.requestRental(request(car), first.getId()));
        }

        List<Throwable> errors = runParallel(tasks);

        long accepted = errors.stream().filter(error -> error == null).count();
        List<Throwable> rejected = errors.stream().filter(error -> error != null).toList();
        assertThat(accepted).isEqualTo(cap);
        assertThat(rejected).hasSize(PARALLEL_REQUESTS - cap)
                .allSatisfy(error -> assertThat(error).isInstanceOf(OpenRentalRequestLimitException.class));
        assertThat(rentalRepository.countByUserIdAndStatusAndIsDeletedFalse(first.getId(), RentalStatus.REQUESTED))
                .isEqualTo(cap);
    }

    @Test
    @DisplayName("does not make different users wait on or count against each other")
    void usersAreIndependent() throws Exception {
        int cap = properties.maxOpenRequestsPerUser();
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < PARALLEL_REQUESTS; i++) {
            Car car = cars.get(i);
            tasks.add(() -> rentalService.requestRental(request(car), first.getId()));
        }
        for (int i = 0; i < PARALLEL_REQUESTS; i++) {
            Car car = cars.get(PARALLEL_REQUESTS + i);
            tasks.add(() -> rentalService.requestRental(request(car), second.getId()));
        }

        runParallel(tasks);

        assertThat(rentalRepository.countByUserIdAndStatusAndIsDeletedFalse(first.getId(), RentalStatus.REQUESTED))
                .isEqualTo(cap);
        assertThat(rentalRepository.countByUserIdAndStatusAndIsDeletedFalse(second.getId(), RentalStatus.REQUESTED))
                .isEqualTo(cap);
    }

    @Test
    @DisplayName("a cancelled request frees a slot for the next request")
    void cancelledRequestFreesSlot() throws Exception {
        int cap = properties.maxOpenRequestsPerUser();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < cap; i++) {
            ids.add(rentalService.requestRental(request(cars.get(i)), first.getId()).id());
        }

        rentalService.cancelRental(ids.get(0), first.getId(), "changed my mind");

        assertThat(rentalService.requestRental(request(cars.get(cap)), first.getId()).id()).isNotNull();
    }
}
