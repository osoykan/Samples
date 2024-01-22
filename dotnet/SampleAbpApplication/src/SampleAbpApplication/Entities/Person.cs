using System.ComponentModel.DataAnnotations.Schema;

using Abp.Domain.Entities;

namespace SampleAbpApplication.Entities
{
    [Table("Person")]
    public class Person : Entity
    {
        private Person()
        {
        }

        public Person(string name) : this()
        {
            Name = name;
        }

        public virtual string Name { get; set; }
    }
}
