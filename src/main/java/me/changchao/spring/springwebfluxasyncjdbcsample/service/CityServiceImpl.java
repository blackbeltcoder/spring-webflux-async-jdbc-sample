package me.changchao.spring.springwebfluxasyncjdbcsample.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import me.changchao.spring.springwebfluxasyncjdbcsample.domain.City;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Component("cityService")
class CityServiceImpl implements CityService {

	@Autowired
	private CityRepository cityRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	@Qualifier("jdbcScheduler")
	private Scheduler jdbcScheduler;

	@Override
	public Mono<City> getCity(String name, String country) {
		// city or city2,which is the better way?
		Mono<City> city = Mono
				.fromCallable(() -> this.cityRepository.findByNameAndCountryAllIgnoringCase(name, country))
				.publishOn(jdbcScheduler);

		Mono<City> city2 = Mono
				.defer(() -> Mono.just(this.cityRepository.findByNameAndCountryAllIgnoringCase(name, country)))
				.subscribeOn(jdbcScheduler);

		// https://spring.io/blog/2016/07/20/notes-on-reactive-programming-part-iii-a-simple-http-server-application
		Mono<City> city3 = Mono
				.fromCallable(() -> this.cityRepository.findByNameAndCountryAllIgnoringCase(name, country))
				.subscribeOn(jdbcScheduler);

		return city;
	}

	@Override
	public Flux<City> findAll() {
		Flux<City> defer = Flux.defer(() -> Flux.fromIterable(this.cityRepository.findAll()));
		return defer.subscribeOn(jdbcScheduler);
	}

	@Override
	public Mono<City> add(String name, String country) {
		return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
			City city = new City(name, country);
			city.setState("state");
			city.setMap("map");
			City savedCity = cityRepository.save(city);
			return savedCity;
		})).subscribeOn(jdbcScheduler);
	}
}