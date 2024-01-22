using System;

using Core.Bus;

namespace Products.Commands
{
    public class CreateProductCommand : Command
    {
        public string Name { get; set; }

        public string Code { get; set; }

        public string Barcode { get; set; }

        public void Validate()
        {
            if (string.IsNullOrEmpty(Name)) throw new ArgumentNullException(nameof(Name));
        }
    }
}
