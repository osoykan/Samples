using Core;
using Core.Bus;

namespace Products.Events
{
    public class ProductCreatedEvent : Event
    {
        public string Barcode;
        public string Code;
        public string Name;

        public ProductCreatedEvent(string barcode, string code, string name)
        {
            Barcode = barcode;
            Code = code;
            Name = name;
        }
    }
}
