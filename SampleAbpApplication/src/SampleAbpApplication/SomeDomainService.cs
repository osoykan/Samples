using System.Linq;

using Abp.Dapper.Dapper.Repositories;
using Abp.Dependency;
using Abp.Domain.Repositories;
using Abp.Domain.Uow;
using Abp.EntityFramework;
using Abp.EntityFramework.Extensions;

using SampleAbpApplication.DbContexes;
using SampleAbpApplication.Entities;

namespace SampleAbpApplication
{
    public class SomeDomainService : ITransientDependency
    {
        private readonly IDbContextProvider<AnimalDbContext> _animalDbContextProvider;
        private readonly IRepository<Animal> _animalRepository;
        private readonly IDapperRepository<Person> _personDapperRepository;
        private readonly IRepository<Person> _personRepository;
        private readonly IUnitOfWorkManager _unitOfWorkManager;

        public SomeDomainService(
            IUnitOfWorkManager unitOfWorkManager,
            IRepository<Person> personRepository,
            IRepository<Animal> animalRepository,
            IDbContextProvider<AnimalDbContext> animalDbContextProvider,
            IDapperRepository<Person> personDapperRepository)
        {
            _unitOfWorkManager = unitOfWorkManager;
            _personRepository = personRepository;
            _animalRepository = animalRepository;
            _animalDbContextProvider = animalDbContextProvider;
            _personDapperRepository = personDapperRepository;
        }

        public void DoSomeStuff()
        {
            using (IUnitOfWorkCompleteHandle uow = _unitOfWorkManager.Begin())
            {
                _personRepository.Insert(new Person("Oğuzhan"));
                _personRepository.Insert(new Person("Ekmek"));

                _animalRepository.Insert(new Animal("Kuş"));
                _animalRepository.Insert(new Animal("Kedi"));

                _animalDbContextProvider.GetDbContext().Animals.Add(new Animal("Kelebek"));

                _unitOfWorkManager.Current.SaveChanges();

                Animal animal = _animalRepository.FirstOrDefault(x => x.Name == "Kuş");

                Person person = _personDapperRepository.Get(1);

                Person anotherPerson = _personRepository.Nolocking(persons => persons.FirstOrDefault(x => x.Name == "Ekmek"));

                uow.Complete();
            }
        }
    }
}
