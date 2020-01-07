package sparksoniq.jsoniq.runtime.iterator.functions.datetime.components;

import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.rumbledb.api.Item;
import sparksoniq.exceptions.InvalidTimezoneException;
import sparksoniq.exceptions.IteratorFlowException;
import sparksoniq.jsoniq.item.ItemFactory;
import sparksoniq.jsoniq.runtime.iterator.RuntimeIterator;
import sparksoniq.jsoniq.runtime.iterator.functions.base.LocalFunctionCallIterator;
import sparksoniq.jsoniq.runtime.metadata.IteratorMetadata;
import sparksoniq.semantics.DynamicContext;

import java.util.List;

public class AdjustTimeToTimezone extends LocalFunctionCallIterator {

    private static final long serialVersionUID = 1L;
    private Item _timeItem = null;
    private Item _timezone = null;

    public AdjustTimeToTimezone(
            List<RuntimeIterator> arguments,
            IteratorMetadata iteratorMetadata
    ) {
        super(arguments, iteratorMetadata);
    }

    @Override
    public Item next() {
        if (this._hasNext) {
            this._hasNext = false;
            if (_timezone == null && this._children.size() == 1)
                return ItemFactory.getInstance()
                    .createTimeItem(_timeItem.getDateTimeValue().withZone(DateTimeZone.UTC), true);
            if (_timezone == null) {
                if (_timeItem.hasTimeZone())
                    return ItemFactory.getInstance()
                        .createTimeItem(
                            _timeItem.getDateTimeValue().withZoneRetainFields(_timeItem.getDateTimeValue().getZone()),
                            false
                        );
                return ItemFactory.getInstance().createTimeItem(_timeItem.getDateTimeValue(), _timeItem.hasTimeZone());
            } else {
                if (this.checkTimeZoneArgument())
                    throw new InvalidTimezoneException("Invalid timezone", getMetadata());
                if (_timeItem.hasTimeZone())
                    return ItemFactory.getInstance()
                        .createTimeItem(
                            _timeItem.getDateTimeValue()
                                .withZone(
                                    DateTimeZone.forOffsetHoursMinutes(
                                        _timezone.getDurationValue().getHours(),
                                        _timezone.getDurationValue().getMinutes()
                                    )
                                ),
                            true
                        );
                return ItemFactory.getInstance()
                    .createTimeItem(
                        _timeItem.getDateTimeValue()
                            .withZoneRetainFields(
                                DateTimeZone.forOffsetHoursMinutes(
                                    _timezone.getDurationValue().getHours(),
                                    _timezone.getDurationValue().getMinutes()
                                )
                            ),
                        true
                    );
            }

        } else
            throw new IteratorFlowException(
                    RuntimeIterator.FLOW_EXCEPTION_MESSAGE + " adjust-time-to-timezone function",
                    getMetadata()
            );
    }

    @Override
    public void open(DynamicContext context) {
        super.open(context);
        _timeItem = this.getSingleItemFromIterator(
            this._children.get(0)
        );
        if (this._children.size() == 2) {
            _timezone = this.getSingleItemFromIterator(
                this._children.get(1)
            );
        }
        this._hasNext = _timeItem != null;
    }

    private boolean checkTimeZoneArgument() {
        return (Math.abs(_timezone.getDurationValue().toDurationFrom(Instant.now()).getMillis()) > 50400000)
            ||
            (Double.compare(
                _timezone.getDurationValue().getSeconds() + _timezone.getDurationValue().getMillis() * 1.0 / 1000,
                0
            ) != 0);
    }
}