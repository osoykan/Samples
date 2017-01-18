using System.ComponentModel.DataAnnotations.Schema;

using Abp.Domain.Entities;

namespace SampleAbpApplication.Entities
{
    [Table("Animal")]
    public class Animal : Entity
    {
        private Animal()
        {
        }

        public Animal(string name) : this()
        {
            Name = name;
        }

        public virtual string Name { get; set; }
    }
}
