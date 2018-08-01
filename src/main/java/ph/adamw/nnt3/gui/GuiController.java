/*
 * MIT License
 *
 * Copyright (c) 2018 awphi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ph.adamw.nnt3.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ph.adamw.nnt3.mazer.Mazer;
import ph.adamw.nnt3.mazer.MazerEvolution;
import ph.adamw.nnt3.evolution.neural.NeuralNetSettings;
import ph.adamw.nnt3.gui.grid.LiveGrid;

import java.text.DecimalFormat;

@NoArgsConstructor
public class GuiController {
	@FXML
	private ListView<MazerListEntry> mazerListView;
	// -- Number Fields --
	@FXML
	private TextField hiddenLayersSizeField;
	@FXML
	private TextField hiddenLayersAmountField;
	@FXML
	private TextField generationSizeField;
    @FXML
	private TextField gridRowsField;
    @FXML
	private TextField gridColsField;
	// --

	@FXML
	private VBox evolutionControlsBox;

	@FXML
	private HBox genZeroConfigBox;

	@FXML
	private Button nextGenButton;

	@FXML
	private BorderPane borderPane;

	private final LiveGrid grid = new LiveGrid(6, 6);

	private MazerEvolution mainEvo;

	private final static DecimalFormat TWO_DP = new DecimalFormat("##.00");

	@Getter
	private Mazer playingMazer;

	@FXML
	private void initialize() {
	    borderPane.setCenter(grid);

		gridColsField.setText(grid.getCols() + "");
		gridRowsField.setText(grid.getRows() + "");

	    final TextField[] numberFields = {hiddenLayersSizeField, hiddenLayersAmountField, generationSizeField, gridRowsField, gridColsField};

		for (TextField numberField : numberFields) {
			numberField.setTextFormatter(new TextFormatter<>(GuiUtils.NUMBER_FIELD_OPERATOR));
		}
	}

	public void onUpdateGridPress(ActionEvent actionEvent) {
		if(!grid.isEditable()) {
			//TODO print error msg in bar here
			return;
		}

		final Integer c = GuiUtils.getBoundedIntFromField(gridColsField, 48, 6);
		final Integer r = GuiUtils.getBoundedIntFromField(gridRowsField, 48, 6);

		if(GuiUtils.anyObjectNull(c, r)) {
			//TODO throw bad input error message in bar here

			return;
		}

		grid.setSize(c, r);

		gridColsField.setText(c.toString());
		gridRowsField.setText(r.toString());
	}

	@SuppressWarnings("SynchronizeOnNonFinalField")
	public void onNextGenPressed(ActionEvent actionEvent) {
		if(mainEvo != null) {
			//TODO clear the old list view here
			mainEvo.start(1, true);
		} else {
			if(!grid.isValid()) {
				//TODO throw error message in bar here
				return;
			}

			final Integer amount = GuiUtils.getBoundedIntFromField(hiddenLayersAmountField, 5, 1);
			final Integer size = GuiUtils.getBoundedIntFromField(hiddenLayersSizeField, 5, 1);
			final Integer genSize = GuiUtils.getBoundedIntFromField(generationSizeField, 24, 1);

			if(GuiUtils.anyObjectNull(amount, size, genSize)) {
				//TODO throw error message in bar here
				return;
			}

			final NeuralNetSettings s = Mazer.STATIC_SETTINGS;
			final NeuralNetSettings settings =
					new NeuralNetSettings(s.getInputs(), amount, size, s.getOutputs(), s.getMutationRate(), s.getActivationFunction());

			mainEvo = new MazerEvolution(grid.asDataGrid(), settings, genSize);
			mainEvo.start(1, true);

			grid.setEditable(false);

			evolutionControlsBox.getChildren().remove(genZeroConfigBox);

			//TODO add new buttons to evo button box - run 5 generations and run X generations
		}

		nextGenButton.setDisable(true);

		synchronized (mainEvo) {
			if(!mainEvo.isDone()) {
				try {
					mainEvo.wait();
				} catch (InterruptedException ignored) {}
			}
		}

		nextGenButton.setText("Run Generation " + mainEvo.getGenerationCount());
		nextGenButton.setDisable(false);

		//TODO (FUN) store the evolutionary path of each winner so we can see a sort of family tree, this could store just names or the Mazers themselves

		// Remove old items
		mazerListView.getItems().clear();
		// They're done - add them to the ListView ready for previews!
		for(Mazer i : mainEvo.getGeneration().getMembers()) {
			mazerListView.getItems().add(mazerListView.getItems().size(),
					new MazerListEntry(i.getThreadName() + " @ " + TWO_DP.format(i.getFitness()) + " fit", i));
		}
	}

	public void mazerListViewClicked(MouseEvent mouseEvent) {
		if(mouseEvent.getTarget() instanceof MazerListEntry) {
			grid.setEditable(false);

			final MazerListEntry e = (MazerListEntry) mouseEvent.getTarget();

			if(playingMazer != null) {
				playingMazer.stop();
			}

			playingMazer = e.getMazer();
			//TODO read in interval information here
			playingMazer.playOnGrid(grid, 100);
		}
	}
}
